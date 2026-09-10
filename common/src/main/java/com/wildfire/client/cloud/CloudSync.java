/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.wildfire.client.cloud;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.serialization.JsonOps;
import com.mojang.util.InstantTypeAdapter;
import com.wildfire.api.WildfireAPI;
import com.wildfire.client.config.ClientConfig;
import com.wildfire.client.contributors.Contributor;
import com.wildfire.client.contributors.ContributorDeserializer;
import com.wildfire.common.LoaderAgnostics;
import com.wildfire.common.WildfireGender;
import com.wildfire.common.WildfireLang;
import com.wildfire.common.config.enums.SyncVerbosity;
import com.wildfire.common.entitydata.PlayerConfig;
import com.wildfire.common.entitydata.PlayerConfigHolder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jspecify.annotations.Nullable;

/// Utility class for managing syncing player data to/from the cloud, even if the current connected server doesn't
/// have the mod installed.
public final class CloudSync {
    private CloudSync() {
        throw new UnsupportedOperationException();
    }

    private static Instant lastSync = Instant.EPOCH;
    private static final List<Instant> fetchErrors = new ArrayList<>();
    private static @Nullable Instant disableFetchingUntil;
    private static @Nullable CloudAuth auth;

    private static final Object AUTH_LOCK = new Object();
    private static final Object SYNC_LOCK = new Object();
    private static final Executor EXECUTOR = Util.ioPool().forName(WildfireAPI.MODID + "$cloudSync");
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .registerTypeAdapter(Contributor.class, new ContributorDeserializer())
        .create();

    private static final HttpClient CLIENT = Util.make(() -> {
        var builder = HttpClient.newBuilder();
        // Use HTTP/1.1 in a development environment if the sync server is not running over https; this is because
        // HttpClient's default of HTTP/2.0 causes issues when making a PUT request to a FastAPI server running
        // over an unencrypted HTTP connection.
        if (LoaderAgnostics.INSTANCE.isDevelopmentEnv() && getCloudServer().startsWith("http://")) {
            builder.version(HttpClient.Version.HTTP_1_1);
        }
        builder.connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL);
        return builder.build();
    });

    private static final String USER_AGENT = Util.make(new StringJoiner(" "), joiner -> {
        joiner.add("WildfireGender/" +  StringUtils.split(LoaderAgnostics.INSTANCE.getModVersion(WildfireAPI.MODID), '+')[0]);
        joiner.add("Minecraft/" + LoaderAgnostics.INSTANCE.getModVersion(Identifier.DEFAULT_NAMESPACE));
        joiner.add(LoaderAgnostics.INSTANCE.name() + "/" + LoaderAgnostics.INSTANCE.getLoaderVersion());
    }).toString();

    private static final Queue<QueuedFetch> QUEUED = new ConcurrentLinkedDeque<>();
    private static final Cache<UUID, Optional<JsonObject>> FETCH_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10)).concurrencyLevel(6).build();

    private static final String DEFAULT_CLOUD_URL = "https://wfgm.celestialfault.dev/v2";
    private static final Duration SYNC_COOLDOWN = Duration.ofSeconds(10);

    /// @return `true` if the last [`sync`][#sync(PlayerConfigHolder)] was within the last 10 seconds
    public static boolean syncOnCooldown() {
        return lastSync.plus(SYNC_COOLDOWN).isAfter(Instant.now());
    }

    /// @return A [SyncUnavailable] enum indicating the reason for syncing being unavailable, or `null` if available
    public static @Nullable SyncUnavailable unavailableReason() {
        var sessionUuid = Minecraft.getInstance().getUser().getProfileId();
        // offline mode sessions use a version 3 UUID
        if(sessionUuid.version() != 4) {
            return SyncUnavailable.INVALID_ACCOUNT;
        }

        if(CloudUtils.hasTheSessionServiceBeenTamperedWith()) {
            return SyncUnavailable.INVALID_ACCOUNT;
        }

        var client = Minecraft.getInstance();
        var connection = client.getConnection();
        if(connection == null) {
            return null;
        }

        //~ if >=26.2 'connection.getConnection().isEncrypted()' -> 'connection.onlineMode()'
        if(!client.isLocalServer() && !connection.onlineMode()) {
            return SyncUnavailable.OFFLINE_SERVER;
        }

        return null;
    }

    /// @return `true` if syncing is available; this method is shorthand for `unavailableReason() == null`.
    public static boolean isAvailable() {
        return unavailableReason() == null;
    }

    /// @return `true` if syncing is enabled; this will always return `false` if [`syncing is unavailable`][#isAvailable()].
    public static boolean isEnabled() {
        return isAvailable() && ClientConfig.config().cloudSync().enabled().get();
    }

    /// @return The URL of the sync server currently being used
    public static String getCloudServer() {
        String url = ClientConfig.config().cloudSync().server().get();
        return url.isBlank() ? DEFAULT_CLOUD_URL : url;
    }

    private static void markFetchError() {
        fetchErrors.add(Instant.now());
        fetchErrors.removeIf(e -> e.plus(30, ChronoUnit.SECONDS).isBefore(Instant.now()));
        if(fetchErrors.size() >= 5) {
            WildfireGender.LOGGER.error("Too many recent sync errors, disabling future lookups for 5 minutes");
            disableFetchingUntil = Instant.now().plus(5, ChronoUnit.MINUTES);
        }
    }

    private static boolean isFetchingDisabled(boolean ignoreConfig) {
        if(!isAvailable()) return true;
        if(!ignoreConfig && !isEnabled()) return true;
        return disableFetchingUntil != null && disableFetchingUntil.isAfter(Instant.now());
    }

    private static HttpRequest.Builder createRequest(URI uri) {
        WildfireGender.LOGGER.debug("Connecting to {}", uri);
        return HttpRequest.newBuilder(uri)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(5));
    }

    @ApiStatus.Internal
    public static CompletableFuture<Map<UUID, Contributor>> getContributors() {
        return CompletableFuture.supplyAsync(() -> {
            var request = createRequest(URI.create(getCloudServer() + "/contributors")).GET().build();

            HttpResponse<String> response;
            try {
                response = CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
                if(response.statusCode() != HttpURLConnection.HTTP_OK) {
                    WildfireGender.LOGGER.warn("Couldn't fetch contributor list: server responded {}", response.statusCode());
                    return Map.of();
                }
            } catch(Exception e) {
                WildfireGender.LOGGER.warn("Couldn't fetch contributor list", e);
                return Map.of();
            }

            try {
                var json = GSON.fromJson(response.body(), JsonObject.class);
                var interim = json.asMap()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                entry -> UUID.fromString(entry.getKey()),
                                entry -> GSON.fromJson(entry.getValue(), Contributor.class)
                        ));
                return Collections.unmodifiableMap(interim.size() <= 8 ? new Object2ObjectArrayMap<>(interim) : new Object2ObjectOpenHashMap<>(interim));
            } catch(Exception e) {
                WildfireGender.LOGGER.error("Failed to parse contributor list", e);
                return Map.of();
            }
        }, EXECUTOR);
    }

    private static String generateServerId() {
        // https://github.com/hibiii/Kappa/blob/main/src/main/java/hibiii/kappa/Provider.java#L40-L42
        BigInteger intA = new BigInteger(128, new Random());
        BigInteger intB = new BigInteger(128, new Random(System.identityHashCode(new Object())));
        return intA.xor(intB).toString(16);
    }

    @Blocking
    private static String getAuthToken() {
        synchronized(AUTH_LOCK) {
            var client = Minecraft.getInstance();
            if(client.player == null) {
                SyncLog.add(WildfireLang.SYNC_LOG_AUTH_FAILED);
                throw new IllegalStateException("Cannot get a new auth token while the client player is unset");
            }
            if(auth == null || auth.isExpired() || auth.isInvalidForClientPlayer()) {
                WildfireGender.LOGGER.info("Authenticating with Mojang session servers");
                SyncLog.add(WildfireLang.SYNC_LOG_AUTH_MOJANG);

                var sessionService = Minecraft.getInstance().services().sessionService();
                var serverId = generateServerId();
                var session = client.getUser();

                try {
                    sessionService.joinServer(Objects.requireNonNull(session.getProfileId()), session.getAccessToken(), serverId);
                } catch(AuthenticationException e) {
                    SyncLog.add(WildfireLang.SYNC_LOG_AUTH_FAILED);
                    throw new RuntimeException(e);
                }

                WildfireGender.LOGGER.info("Obtaining new authentication token from the cloud sync server");
                SyncLog.add(WildfireLang.SYNC_LOG_AUTH_SYNC);
                String body = Util.make(new JsonObject(), obj -> {
                    obj.addProperty("server_id", serverId);
                    obj.addProperty("username", session.getName());
                }).toString();
                var uri = URI.create(getCloudServer() + "/auth");
                var request = createRequest(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .build();

                var response = CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
                if(response.statusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    SyncLog.add(WildfireLang.SYNC_LOG_AUTH_FAILED);
                    throw new RuntimeException("Failed to authenticate with sync server: " + response.body());
                }

                auth = GSON.fromJson(response.body(), CloudAuth.class);
                if(auth.isInvalidForClientPlayer()) {
                    WildfireGender.LOGGER.warn("Authenticated account {} does not match the current player ({}); you likely have a misbehaving account switcher mod installed!", auth.account(), client.player.getUUID());
                }
                WildfireGender.LOGGER.info("Obtained authentication token for {}, expiry {}", auth.account(), auth.expires());
            }
        }
        return auth.token();
    }

    /// Send the client player config to the cloud sync server for syncing to other players
    ///
    /// @param config The config of the client player
    ///
    /// @return A [CompletableFuture] indicating when the process has finished, or with an exception if
    ///         syncing failed.
    public static CompletableFuture<Void> sync(PlayerConfigHolder config) {
        if(!isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        synchronized(SYNC_LOCK) {
            // Force a 10s cooldown on syncing
            if(syncOnCooldown()) {
                var future = new CompletableFuture<Void>();
                future.completeExceptionally(new SyncingTooFrequentlyException());
                return future;
            }
            lastSync = Instant.now();
        }

        return syncInternal(config, false);
    }

    private static CompletableFuture<Void> syncInternal(PlayerConfigHolder config, boolean resyncing) {
        return CompletableFuture.runAsync(() -> {
            var token = getAuthToken();
            var url = URI.create(getCloudServer() + "/player/" + config.uuid);
            var json = PlayerConfig.CODEC.encodeStart(JsonOps.INSTANCE, config.config()).resultOrPartial().orElseGet(JsonObject::new).toString();

            SyncLog.add(WildfireLang.SYNC_LOG_START);

            var request = createRequest(url)
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Auth-Token", token)
                    .build();

            var response = CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
            if(response.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED && !resyncing) {
                SyncLog.add(WildfireLang.SYNC_LOG_REAUTH);
                WildfireGender.LOGGER.warn("Auth token is invalid, attempting to reauth...");
                auth = null;
                syncInternal(config, true).join();
                return;
            } else if(response.statusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                throw new RuntimeException("Server responded " + response.statusCode() + ": " + response.body());
            }

            WildfireGender.LOGGER.debug("Server responded to update: {}", response.body());
            SyncLog.add(WildfireLang.SYNC_LOG_SUCCESS);
        }, EXECUTOR);
    }

    /// Request that the cloud sync server delete the data stored for the provided client player.
    ///
    /// @param config The config of the client player
    ///
    /// @return A [CompletableFuture] indicating when the process has finished, or with an exception if
    ///         the request failed.
    public static CompletableFuture<Void> deleteProfile(PlayerConfigHolder config) {
        return CompletableFuture.runAsync(() -> {
            var token = getAuthToken();
            var url = URI.create(getCloudServer() + "/player/" + config.uuid);
            var request = createRequest(url).DELETE().header("Auth-Token", token).build();
            var response = CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
            if(response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                SyncLog.add(WildfireLang.SYNC_LOG_NO_PROFILE);
                return;
            } else if(response.statusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                SyncLog.add(WildfireLang.SYNC_LOG_PROFILE_DELETION_FAILED);
                throw new RuntimeException("Server responded " + response.statusCode() + ": " + response.body());
            }
            WildfireGender.LOGGER.debug("Deleted cloud sync profile");
            SyncLog.add(WildfireLang.SYNC_LOG_PROFILE_DELETED);
        }, EXECUTOR);
    }

    /// @see #getProfile(UUID, boolean)
    public static CompletableFuture<@Nullable JsonObject> getProfile(UUID uuid) {
        return getProfile(uuid, false);
    }

    /// Fetch player data from the sync server
    ///
    /// @param uuid The UUID of the player to get data for
    ///
    /// @return A [CompletableFuture] containing a [JsonObject] of the player's data if they have any data
    ///         stored in the sync server, or `null` otherwise.
    ///
    /// @apiNote The provided UUID **must** be [`version 4`][UUID#version()], otherwise the request will fail.
    ///
    /// @see #queueFetch(UUID)
    public static CompletableFuture<@Nullable JsonObject> getProfile(UUID uuid, boolean ignoreConfig) {
        if(isFetchingDisabled(ignoreConfig)) {
            return CompletableFuture.completedFuture(null);
        }
        if(uuid.version() != 4) {
            // some servers (namely hypixel) use non-v4 uuids for their npcs; the sync server will immediately reject
            // such uuids with a 422 response, so don't bother trying to fetch them.
            return CompletableFuture.completedFuture(null);
        }
        var cached = FETCH_CACHE.getIfPresent(uuid);
        //noinspection OptionalAssignedToNull
        if(cached != null) {
            return CompletableFuture.completedFuture(cached.orElse(null));
        }

        return CompletableFuture.supplyAsync(() -> {
            var url = URI.create(getCloudServer() + "/player/" + uuid);
            var request = createRequest(url).GET().build();
            var response = CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
            if(response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND || response.statusCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                WildfireGender.LOGGER.debug("Server replied no data for {}", uuid);
                FETCH_CACHE.put(uuid, Optional.empty());
                return null;
            } else if(response.statusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                markFetchError();
                throw new RuntimeException("Server responded " + response.statusCode() + ": " + response.body());
            }

            SyncLog.add(WildfireLang.SYNC_LOG_SINGLE_PROFILE, SyncVerbosity.SHOW_FETCHES);

            var data = GSON.fromJson(response.body(), JsonObject.class);
            FETCH_CACHE.put(uuid, Optional.of(data));
            return data;
        }, EXECUTOR);
    }

    /// Fetch data for multiple players from the sync server.
    ///
    /// @param uuids A collection of between 2 and 20 UUIDs to fetch player data for
    ///
    /// @return A [CompletableFuture] containing a map of player UUIDs to their synced data; any provided
    ///         player UUIDs without any sync data will not be included in the returned map.
    ///
    /// @apiNote All UUIDs **must** be [`version 4`][UUID#version()], otherwise the request will fail.
    ///
    /// @see #queueFetch(UUID)
    public static CompletableFuture<Map<UUID, JsonObject>> getMultiple(Collection<UUID> uuids) {
        return CompletableFuture.supplyAsync(() -> {
            if(isFetchingDisabled(false)) {
                return Collections.emptyMap();
            }

            var url = URI.create(getCloudServer() + "/players");
            var json = new JsonArray();
            uuids.forEach(uuid -> json.add(uuid.toString()));
            var request = createRequest(url)
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .build();
            var response = CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
            if(response.statusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                markFetchError();
                throw new RuntimeException("Server responded " + response.statusCode() + ": " + response.body());
            }

            SyncLog.add(WildfireLang.SYNC_LOG_MULTIPLE_PROFILES, SyncVerbosity.SHOW_FETCHES);
            var data = GSON.fromJson(response.body(), BulkFetch.class).users();
            uuids.forEach(uuid -> FETCH_CACHE.put(uuid, Optional.ofNullable(data.get(uuid))));
            return data;
        }, EXECUTOR);
    }

    /// Add a UUID to the fetch queue; this may be requested individually or in bulk, depending on how many other
    /// users are queued to be fetched.
    ///
    /// Queued queries are currently sent once every 2 seconds (or every 40th tick) in batches of up to 20 at once.
    ///
    /// @param uuid The UUID of the user to fetch
    ///
    /// @return A [CompletableFuture] containing a [JsonObject] of the relevant player data,
    ///         which will be completed once the next queued batch is finished.
    public static CompletableFuture<@Nullable JsonObject> queueFetch(UUID uuid) {
        if(!isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        var cached = FETCH_CACHE.getIfPresent(uuid);
        //noinspection OptionalAssignedToNull
        if(cached != null) {
            return CompletableFuture.completedFuture(cached.orElse(null));
        }
        if(uuid.version() != 4) {
            // some servers (namely hypixel) use non-v4 uuids for their npcs; the sync server will immediately reject
            // such uuids with a 422 response, so don't bother trying to fetch them.
            return CompletableFuture.completedFuture(null);
        }

        var future = new CompletableFuture<@Nullable JsonObject>();
        QUEUED.add(new QueuedFetch(uuid, future));
        return future;
    }

    /// Sends up to 20 [`queued sync queries`][#queueFetch(UUID)]
    ///
    /// @apiNote This method is not intended to be used by other mods.
    @ApiStatus.Internal
    public static void sendNextQueueBatch() {
        if(QUEUED.isEmpty()) {
            return;
        }

        final var toFetch = new ArrayList<QueuedFetch>();
        for(int i = 0; i < 20; i++) {
            var next = QUEUED.poll();
            if(next == null) break;
            toFetch.add(next);
        }

        // If there's 3 or fewer players in the queue, just send them all individually so that the requests
        // can be cached easier
        if(toFetch.size() < 4) {
            WildfireGender.LOGGER.debug("Sending {} queued sync queries", toFetch.size());
            toFetch.forEach(queued -> CompletableFuture.runAsync(() -> {
                try {
                    queued.future().complete(getProfile(queued.uuid()).join());
                } catch(Exception e) {
                    var actualException = e instanceof CompletionException ce ? ce.getCause() : e;
                    queued.future().completeExceptionally(actualException);
                }
            }));
            return;
        }

        WildfireGender.LOGGER.debug("Fetching sync data for {} players in bulk", toFetch.size());
        CompletableFuture.runAsync(() -> {
            Map<UUID, JsonObject> result;
            try {
                result = getMultiple(toFetch.stream().map(QueuedFetch::uuid).toList()).join();
            } catch(Exception e) {
                var actualException = e instanceof CompletionException ce ? ce.getCause() : e;
                toFetch.forEach(queued -> queued.future().completeExceptionally(actualException));
                return;
            }

            toFetch.forEach(queued -> queued.future().complete(result.get(queued.uuid())));
        });
    }
}
