# alumbra.validator

A validator for GraphQL ASTs as produced by [alumbra.parser][parser], based on
[invariant][invariant].

[![Build Status](https://travis-ci.org/alumbra/alumbra.validator.svg?branch=master)](https://travis-ci.org/alumbra/alumbra.validator)
[![Clojars Project](https://img.shields.io/clojars/v/alumbra/validator.svg)](https://clojars.org/alumbra/validator)

[parser]: https://github.com/alumbra/alumbra.parser
[invariant]: https://github.com/xsc/invariant

## Usage

```clojure
(require '[alumbra.validator :as v])
```

A validator is based upon an [analyzed GraphQL schema][alumbra-analyzer] and
can be created as follows:

```clojure
(def validate-document
  (v/validator analyzed-schema))
```

The validator function can be called with a value conforming to
`:alumbra/document` and will return either `nil` (if the GraphQL query
is valid) or a map with `:alumbra/validation-errors`.

[alumbra-analyzer]: https://github.com/alumbra/alumbra.analyzer

## Issues

Issue tracking for alumbra is centralized at [alumbra/alumbra][issues].

[issues]: https://github.com/alumbra/alumbra/issues

## License

```
MIT License

Copyright (c) 2016 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
