### [DJ: Distributed JIT](https://github.com/matthewfl/dj)

This repo represents a work in progress.  At the moment this is not likely useful to anyone.

## License
This is dual licensed under the [LGPL v3](http://www.gnu.org/licenses/lgpl-3.0.txt) and the [Apache license v2](http://www.apache.org/licenses/LICENSE-2.0.txt).

A modified version of [Javassist](http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/) version 3.19 is included in the src/main/java/javassist directory.  It is licensed under the LGPL version 2.1 or later, the Apache license v2 and the Mozilla public license v1.1.

Some core classes from the jdkv8 have been copied and modified into the edu.berkeley.dj.internal.coreclazz folder, these are licensed under gplv2.



## Examples

In the src/examples/simpleMR directory, there is an example of how one could build a map reduce framework on top
of the DJ Primitives.