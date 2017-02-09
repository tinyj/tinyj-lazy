
tinyj-lazy [![build status](https://travis-ci.org/tinyj/tinyj-lazy.svg?branch=master)](https://travis-ci.org/tinyj/tinyj-lazy)
==========

A thread and exception safe implementation of lazy (aka on-demand) value
initialization.

Features:

- Initialization is guaranteed to run only once in multi-threaded scenarios
  as well in case of raised exceptions
- The initializer is released after initialization so it can be collected by
  the garbage collector. This is still true if initialization fails.
- Exception raised during initialization are propagated in the same thread
  (this is important if a `InterruptedException` for that thread is raised
  during initialization)
- Exceptions raised during initialization are kept for later inspection/logging


## API documentation

You can find the API documentation [here](APIdoc.md).


## Examples

See tests.


## License

Copyright 2016 Eric Karge <e.karge@struction.de>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
