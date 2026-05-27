Build
=====

This @PLUGIN@ plugin is built with Bazel standard in tree [build](https://gerrit-review.googlesource.com/Documentation/dev-build-plugins.html#_bazel_in_tree_driven).

Clone (or link) this plugin to the `plugins` directory of Gerrit's
source tree.
The plugin depends on [events-broker](https://gerrit.googlesource.com/modules/events-broker)
which is linked directly from source with the same 'in-tree' plugin structure.

From the Gerrit source tree issue the command:

```
  bazel build plugins/@PLUGIN@
```

The output is created in

```
  bazel-bin/plugins/@PLUGIN@/@PLUGIN@.jar
```

This project can be imported into the Eclipse IDE.
Add the plugin name to the `CUSTOM_PLUGINS` set in
Gerrit core in `tools/bzl/plugins.bzl`, and execute:

```
  ./tools/eclipse/project.py
```

To execute the tests run:

```
  bazel test plugins/@PLUGIN@:websession-broker_tests
```

How to build the Gerrit Plugin API is described in the [Gerrit
documentation](../../../Documentation/dev-bazel.html#_extension_and_plugin_api_jar_files).
