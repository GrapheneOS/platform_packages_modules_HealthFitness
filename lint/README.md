# Health Fitness Lint Checks for AOSP

Custom Android Lint checks are written here to be executed against Health Fitness
Java and Kotlin source code. These will appear as part of errorprone builds,
which are notably ran as part of pre/post-submit testing of Health Fitness code.

## How to run Health Fitness lint checks against the code base

While lint checks should be automatically run by Gerrit, you may want to run it manually on the
code base.

Step 1: Build the lint report:
```
m HealthFitnessLintChecker
```

Step 2: Find your lint output:
```
croot;
find out/soong/.intermediates/packages/modules/HealthFitness/ -type f -name "*lint-report*" -print;
```

Or:
```
aninja -t query HealthFitnessLintChecker
```

Step 3: Identify the lint report you want to view the results of, typically in the following format:
```
out/soong/.intermediates/packages/modules/HealthFitness/lint/android_common/<run-identifier>/lint/lint-report.html
```

Step 4: Open the file in your favorite web browser.

## How to run Health Fitness lint unit tests

Unit tests can be ran with the following command:
```
atest HealthFitnessLintCheckerTest --host
```

## How to add lint checks to modules

1. Add the following `lint` attribute to the module definition:
```
java_library_static {
    name: "my-module",
    ...
    lint: {
        extra_check_modules: ["HealthFitnessLintChecker"],
    },
}
```
2. Run the following command to verify that the report is being correctly built:
```
m out/soong/.intermediates/path/to/my-module/android_common/lint/lint-report.html
```
   (Lint report can be found in the same path, i.e. `out/../lint-report.html`)

3. Now lint issues should appear on gerrit!

## Documentation

- [Android Lint Docs](https://googlesamples.github.io/android-custom-lint-rules/)
- [Custom lint creation example from Android SystemUI team](https://g3doc.corp.google.com/company/teams/android-sysui/general_guides/writing_a_linter.md?cl=head)
- [Lint Check Unit Testing](https://googlesamples.github.io/android-custom-lint-rules/api-guide/unit-testing.md.html)
- [Android Lint source files](https://source.corp.google.com/studio-main/tools/base/lint/libs/lint-api/src/main/java/com/android/tools/lint/)
- [PSI source files](https://github.com/JetBrains/intellij-community/tree/master/java/java-psi-api/src/com/intellij/psi)
- [UAST source files](https://upsource.jetbrains.com/idea-ce/structure/idea-ce-7b9b8cc138bbd90aec26433f82cd2c6838694003/uast/uast-common/src/org/jetbrains/uast)
- [IntelliJ plugin for viewing PSI tree of files](https://plugins.jetbrains.com/plugin/227-psiviewer)
