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
        "//plugins/gerrit-bsl-license",
    ],
)

gerrit_plugin_tests(
    name = "websession-broker_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    plugin = "websession-broker",
    tags = ["websession-broker"],
    deps = ["//plugins/events-broker"],
)

java_library(
    name = "events-broker-neverlink",
    neverlink = 1,
    exports = ["//plugins/events-broker"],
)