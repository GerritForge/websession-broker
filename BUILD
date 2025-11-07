load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

gerrit_plugin(
    name = "websession-broker",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: websession-broker",
        "Gerrit-HttpModule: com.gerritforge.gerrit.plugins.websession.broker.BrokerBasedWebSession$Module",
        "Implementation-Title: Broker WebSession",
        "Implementation-URL: https://review.gerrithub.io/admin/repos/GerritForge/plugins_websession-broker",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        ":events-broker-neverlink",
    ],
)

junit_tests(
    name = "websession-broker_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    resources = glob(["src/test/resources/**/*"]),
    tags = ["websession-broker"],
    deps = [
        ":websession-broker__plugin_test_deps",
    ],
)

java_library(
    name = "websession-broker__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":websession-broker__plugin",
        "@mockito//jar",
        "//plugins/events-broker",
    ],
)

java_library(
    name = "events-broker-neverlink",
    neverlink = 1,
    exports = ["//plugins/events-broker"],
)
