# Contribution Guidelines

## Before you open an issue

- Make sure an issue for your issue or feature request hasn't already been opened, and additionally is not
  listed on the [frequently asked questions](./README.md#frequently-asked-questions)
- Include as much detail as possible; steps to reproduce an issue, crash logs, etc.
- If your issue relates to mod compatibility, please ensure the other involved mods are not the cause first!

## Contributing translations

Some ground rules for contributed translations:

- Translations **may not** be made using machine translation of any kind, either in part or in full;
  it is preferred that you are fluent enough in the language to be able to translate the text on your own.
- **Localize**, don't just translate the text verbatim. If something wouldn't make sense in the language you're translating
  to, reword it to make more sense (while also keeping it similar to the spirit of the original English text).

The original English locale files can be found in `{loader}/versions/{version}/src/main/generated/` for each relevant loader;
for example, [the English locale for Fabric 26.1](./fabric/versions/26.1/src/main/generated/assets/female_gender_mod/lang/en_us.json).

Contributed translations should be placed within the `common` project's resources directory; for example,
[the LOLCAT translation](./common/src/main/resources/assets/female_gender_mod/lang/lol_us.json).

## Contributing code changes

**Contributions made using Large Language Models/LLMs (or any other similar form of Generative AI) will be rejected.**

If you're contributing a feature (or the changes are otherwise large enough to need more review than
a simple bug fix), please open an issue or otherwise ask for feedback beforehand.

This project is split into a `common` project, along with projects for each supported loader (`fabric` and `neoforge`),
with further subprojects for every supported Minecraft version for each project (using [Stonecutter](https://stonecutter.kikugie.dev/)); see
[`./versions.json5`](./versions.json5) for the list of supported versions and the loader(s) they target.

Any loader-specific logic should be limited, with as much of it handled through interfaces like `LoaderAgnostics`
in the `common` module as possible.

If any changes you make result in generated data files needing to be updated, use the `:runData` Gradle task
to regenerate them; **do not manually edit the generated files!**

### Coding conventions

Most of these will be automatically applied if your editor respects `.editorconfig` configurations, but you
should still check to make sure.

- Use 4 spaces for indentation
- Avoid any kind of `*` imports where possible
- Fully-qualified class references should be avoided, except when used in versioned code where it's the only use
  of any such classes, in which case fully-qualified references are preferred.
