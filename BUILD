load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_tests",
)
load("@rules_java//java:defs.bzl", "java_library")

gerrit_plugin(
    name = "websession-broker",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: websession-broker",
        "Gerrit-HttpModule: com.gerritforge.gerrit.plugins.websession.broker.BrokerBasedWebSession$Module",
        "Implementation-Title: Broker WebSession",
        "Implementation-URL: https://github.com/GerritForge/websession-broker",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        ":events-broker-neverlink",
    ],
)

gerrit_plugin_tests(
    name = "websession-broker_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    plugin = "websession-broker",
    tags = ["websession-broker"],
<<<<<<< HEAD   (6e25b2ad08d20e296b32c250b1487df8308dee07 Update links from README.md to LICENSE)
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
=======
    deps = ["//plugins/events-broker"],
>>>>>>> CHANGE (d6e622c389c07194a589375529380096480823c9 Migrate to bazelmod)
)

java_library(
    name = "events-broker-neverlink",
    neverlink = 1,
    exports = ["//plugins/events-broker"],
)