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

package com.wildfire.common;

import com.wildfire.api.WildfireAPI;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public enum WildfireLang {
    ARMOR_TOOLTIP("armor.tooltip"),

    UV_EDITOR("uv_editor"),
    UV_EDITOR_RESET_ALL("uv_editor.reset_defaults_all"),
    UV_EDITOR_RESET("uv_editor.reset_defaults"),
    UV_EDITOR_NO_FACE("uv_editor.no_face_selected"),
    UV_EDITOR_LB("uv_editor.selection.left_breast"),
    UV_EDITOR_RB("uv_editor.selection.right_breast"),
    UV_EDITOR_LB_OVERLAY("uv_editor.selection.left_breast_overlay"),
    UV_EDITOR_RB_OVERLAY("uv_editor.selection.right_breast_overlay"),
    UV_EDITOR_BODY_LAYER("uv_editor.selection.layer_body"),
    UV_EDITOR_JACKET_LAYER("uv_editor.selection.layer_jacket"),

    UV_EDITOR_X_POS("uv_editor.xpos"),
    UV_EDITOR_Y_POS("uv_editor.ypos"),
    UV_EDITOR_WIDTH("uv_editor.width"),
    UV_EDITOR_HEIGHT("uv_editor.height"),
    UV_EDITOR_INCREMENT("uv_editor.increment_tip"),
    UV_EDITOR_ADD("uv_editor.add"),
    UV_EDITOR_REMOVE("uv_editor.remove"),

    UV_SELECTED_DIRECTION("uv.selected_direction"),
    UV_QUAD("uv.quad"),

    CREDITS_TITLE("credits.title"),
    CREDITS_DESCRIPTION("credits.description"),
    CREDITS_GENERAL("credits.general"),
    CREDITS_TRANSLATORS("credits.translators"),

    PLAYER_LIST_TITLE("player_list.title"),
    PLAYER_LIST_SETTINGS("player_list.settings_button"),
    PLAYER_LIST_SYNC_STATUS("player_list.sync_status"),
    PLAYER_LIST_LOADING("player_list.state.loading"),
    PLAYER_LIST_SYNCED("player_list.state.synced"),
    PLAYER_LIST_BOUNCE_MULTIPLIER("player_list.bounce_multiplier"),
    PLAYER_LIST_BREAST_MOMENTUM("player_list.breast_momentum"),
    PLAYER_LIST_SOUNDS("player_list.female_sounds"),

    PLAYER_LIST_MODE("always_show_list"),
    PLAYER_LIST_MODE_MOD_UI("always_show_list.mod_ui_only"),
    PLAYER_LIST_MODE_MOD_UI_TOOLTIP("always_show_list.mod_ui_only.tooltip"),
    PLAYER_LIST_MODE_TAB_LIST("always_show_list.tab_list_open"),
    PLAYER_LIST_MODE_TAB_LIST_TOOLTIP("always_show_list.tab_list_open.tooltip"),
    PLAYER_LIST_MODE_ALWAYS("always_show_list.always"),
    PLAYER_LIST_MODE_ALWAYS_TOOLTIP("always_show_list.always.tooltip"),

    CUSTOMIZATION_TAB_CUSTOMIZATION("breast_customization.tab_customization"),
    CUSTOMIZATION_TAB_PHYSICS("breast_customization.tab_physics"),
    CUSTOMIZATION_TAB_MISC("breast_customization.tab_miscellaneous"),
    CUSTOMIZATION_PRESET_NEW("breast_customization.presets.add_new"),
    CUSTOMIZATION_PRESET_DELETE("breast_customization.presets.delete"),
    CUSTOMIZATION_DUAL_PHYSICS("breast_customization.dual_physics"),


    WARDROBE_TITLE("wardrobe.title"),
    WARDROBE_PLAYERS_USING("wardrobe.players_using_mod"),
    WARDROBE_SLIDER_BREAST_SIZE("wardrobe.slider.breast_size"),
    WARDROBE_SLIDER_SEPARATION("wardrobe.slider.separation"),
    WARDROBE_SLIDER_HEIGHT("wardrobe.slider.height"),
    WARDROBE_SLIDER_DEPTH("wardrobe.slider.depth"),
    WARDROBE_SLIDER_ROTATION("wardrobe.slider.rotation"),
    //TODO: Should these have wardrobe in the name?
    WARDROBE_SLIDER_PITCH("slider.voice_pitch"),
    WARDROBE_SLIDER_BOUNCE("slider.bounce"),
    WARDROBE_SLIDER_FLOPPY("slider.floppy"),


    APPEARANCE_SETTINGS_TITLE("appearance_settings.title"),
    CHAR_SETTINGS_PHYSICS("char_settings.physics"),
    CHAR_SETTINGS_JUMP("char_settings.jump"),
    CHAR_SETTINGS_JUMPING("char_settings.jumping"),
    CHAR_SETTINGS_HIDE_IN_ARMOR("char_settings.hide_in_armor"),
    CHAR_SETTINGS_ARMOR_STAT("char_settings.show_armor_stat"),
    CHAR_SETTINGS_HURT_SOUNDS("char_settings.hurt_sounds"),
    CHAR_SETTINGS_HURT_SOUNDS_TOOLTIP("tooltip.hurt_sounds"),
    CHAR_SETTINGS_OVERRIDE_PHYSICS("char_settings.override_armor_physics"),

    LABEL_GENDER("label.gender"),
    LABEL_ENABLED("label.enabled"),
    LABEL_DISABLED("label.disabled"),
    LABEL_ON("label.on"),
    LABEL_OFF("label.off"),
    LABEL_WITH_CREATOR("label.with_creator"),
    LABEL_WITH_CONTRIBUTOR("label.with_contributor"),
    LABEL_WITH_BOTH("label.with_both"),

    CANCER_AWARENESS_TITLE("cancer_awareness.title"),

    FIRST_TIME_TITLE("first_time_setup.title"),
    FIRST_TIME_DESCRIPTION("first_time_setup.description"),
    FIRST_TIME_NOTICE("first_time_setup.notice"),
    FIRST_TIME_ENABLE("first_time_setup.enable"),
    FIRST_TIME_ENABLE_TOOLTIP("first_time_setup.enable.tooltip"),
    FIRST_TIME_DISABLE("first_time_setup.disable"),

    CLOUD_SETTINGS("cloud_settings"),
    CLOUD_TOOLTIP("cloud.tooltip"),
    CLOUD_UNAVAILABLE_INVALID_ACC("cloud.unavailable.invalid_account"),
    CLOUD_UNAVAILABLE_OFFLINE_SERVER("cloud.unavailable.offline_server"),
    CLOUD_STATUS("cloud.status"),
    CLOUD_AUTOMATIC("cloud.automatic"),
    CLOUD_AUTOMATIC_TOOLTIP("cloud.automatic.tooltip"),
    CLOUD_SYNC("cloud.sync"),
    CLOUD_SYNCING("cloud.syncing"),
    CLOUD_SYNCING_SUCCESS("cloud.syncing.success"),
    CLOUD_SYNCING_FAIL("cloud.syncing.fail"),
    CLOUD_DELETE("cloud.delete"),
    CLOUD_DELETED("cloud.deleted"),
    CLOUD_DELETE_FAILED("cloud.delete_failed"),
    CLOUD_STATUS_LOG("cloud.status_log"),

    CLOUD_DETAILS("cloud_details.title"),
    DETAILS_NEXT_PAGE("details.next_page"),
    DETAILS_PREV_PAGE("details.prev_page"),
    DETAILS_BACK("details.go_back"),

    SYNC_LOG_AUTH_MOJANG("sync_log.authenticating_mojang"),
    SYNC_LOG_AUTH_SYNC("sync_log.authenticating_sync"),
    SYNC_LOG_AUTH_FAILED("sync_log.authentication_failed"),
    SYNC_LOG_REAUTH("sync_log.reauthenticating"),
    SYNC_LOG_FAILED("sync_log.failed_to_sync_data"),
    SYNC_LOG_START("sync_log.attempting_sync"),
    SYNC_LOG_SUCCESS("sync_log.sync_success"),
    SYNC_LOG_TOO_FREQUENT("sync_log.sync_too_frequently"),
    SYNC_LOG_PROFILE_DELETED("sync_log.data_deleted"),
    SYNC_LOG_PROFILE_DELETION_FAILED("sync_log.data_deletion_failed"),
    SYNC_LOG_NO_PROFILE("sync_log.no_data_to_delete"),
    SYNC_LOG_SINGLE_PROFILE("sync_log.get_single_profile"),
    SYNC_LOG_MULTIPLE_PROFILES("sync_log.get_multiple_profiles"),
    SYNC_LOG_VERBOSITY_DEFAULT("sync_log.verbosity.default"),
    SYNC_LOG_VERBOSITY_SHOW_FETCHES("sync_log.verbosity.show_fetches"),

    CONTRIBUTOR_ROLE_MOD_CREATOR("contributor.role.mod_creator"),
    CONTRIBUTOR_ROLE_FABRIC_MAINTAINER("contributor.role.fabric_maintainer"),
    CONTRIBUTOR_ROLE_NEO_MAINTAINER("contributor.role.neoforge_maintainer"),
    CONTRIBUTOR_ROLE_DEVELOPER("contributor.role.developer"),
    CONTRIBUTOR_ROLE_CI_MAINTAINER("contributor.role.ci_maintainer"),
    CONTRIBUTOR_ROLE_TRANSLATOR("contributor.role.translator"),
    CONTRIBUTOR_ROLE_MASCOT("contributor.role.mascot"),
    CONTRIBUTOR_ROLE_FEMALE_VOICE_ACTOR("contributor.role.voice_actor_female"),
    CONTRIBUTOR_ROLE_GENERIC("contributor.role.generic"),

    GENERIC_BRACKETS("generic.brackets"),
    GENERIC_ELLIPSIS_SUFFIX("generic.ellipsis.suffix"),
    GENERIC_CONCAT("generic.concat"),
    GENERIC_SPACE("generic.space"),
    GENERIC_DASH_EXPLANATION("generic.dash_explanation"),

    KEIRA("misc.keira_emberlyn"),
    MISC_F("misc.f"),
    MISC_GM("misc.gm"),

    NOT_IN_WORLD("not_in_world"),
    NOT_IN_WORLD_TITLE("not_in_world.title"),

    HURT_SOUND_SUBTITLE("hurt", "female"),
    KEY_CATEGORY("key.category", "generic"),
    KEY_CONFIG("key", "gender_menu"),
    KEY_TOGGLE("key", "toggle"),

    TOAST_GET_STARTED("toast", "get_started"),

    DEBUG_COMMAND("command.debug"),
    COMMAND_INVALIDATE_CACHE("command.debug.invalidate_cache"),
    COMMAND_INVALIDATE_CACHE_SUCCESS("command.debug.invalidate_cache.success"),
    COMMAND_TARGET("command.debug.target"),
    COMMAND_CACHE("command.debug.cache"),
    COMMAND_FIRST_TIME("command.debug.firsttime"),
    COMMAND_SYNC_VERBOSITY("command.debug.syncverbosity"),
    SINGLE_PLAYER_COMMAND("command.single_player"),
    COMMAND_ARMOR_STAND("command.single_player.armor_stand"),
    COMMAND_ARMOR_STAND_NO_COMPONENT("command.single_player.armor_stand.error.no_component"),
    COMMAND_TRIM("command.single_player.trim"),

    COMMAND_LOOKING_AT("command.looking_at"),
    COMMAND_LOOKING_AT_NONE("command.looking_at.none"),
    COMMAND_LOOKING_AT_UUID("command.looking_at.uuid"),
    COMMAND_LOOKING_AT_TYPE("command.looking_at.type"),
    COMMAND_LOOKING_AT_CLASS("command.looking_at.class"),
    COMMAND_LOOKING_AT_RENDERER("command.looking_at.renderer"),
    COMMAND_LOG_LEVEL("command.log_level"),
    COMMAND_SYNCED_PLAYERS("command.synced_players"),
    COMMAND_ENTITIES("command.entities"),


    ;

    private final String translationKey;

    WildfireLang(String type, String path) {
        this.translationKey = WildfireGender.id(path).toLanguageKey(type);
    }

    WildfireLang(String path) {
        //TODO: Evaluate changing lang key paths to actually using mojang's toLanguageKey helpers
        this.translationKey = WildfireAPI.MODID + "." + path;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public MutableComponent translateColored(TextColor textColor) {
        //~ if >=26.2 'textColor.getValue()' -> 'textColor'
        return translate().withColor(textColor);
    }

    public MutableComponent translateColored(TextColor textColor, Object... args) {
        //~ if >=26.2 'textColor.getValue()' -> 'textColor'
        return translate(args).withColor(textColor);
    }

    //? if <26.2 {
    /*public MutableComponent translateColored(ChatFormatting color) {
        return translate().withStyle(color);
    }
    public MutableComponent translateColored(ChatFormatting color, Object... args) {
        return translate(args).withStyle(color);
    }
    *///?}

    public MutableComponent translate() {
        return Component.translatable(translationKey);
    }

    public MutableComponent translate(Object... args) {
        //Simple filter to auto translate any sub lang entries
        return Component.translatable(translationKey, Arrays.stream(args).map(arg -> arg instanceof WildfireLang lang ? lang.translate() : arg).toArray());
    }

    public MutableComponent line(int line) {
        return Component.translatable(translationKey + ".line" + line);
    }

    public MutableComponent translateShort() {
        return Component.translatable(translationKey + ".short");
    }

    public MutableComponent translateDescription() {
        return Component.translatable(translationKey + ".description");
    }
}
