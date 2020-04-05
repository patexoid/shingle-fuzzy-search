[![Build Status](https://travis-ci.org/patexoid/shingle-fuzzy-search.svg?branch=master)](https://travis-ci.org/patexoid/shingle-fuzzy-search)

# shingle-fuzzy-search
object search, based on https://en.wikipedia.org/wiki/W-shingling.


Object matcher and search based on https://en.wikipedia.org/wiki/W-shingling.
Was optimized  for finding similar ebooks on devices with limited resources like raspberry pi.

## How to use

Use  `com.patex.shingle.ShingleMatcher.isSimilar(Object, Object)` to check are objects similar

Use `com.patex.shingle.ShingleSearch.findSimilar` or `com.patex.shingle.ShingleSearch.findSimilarStream`

Both have cache inside and for both you have posibility to specify external cache storage via `setStorage` method

Because previously it was a part of my other projects and I didn't refactored it yet both have constructor with 
huge amounts of params. For more details check java dock

## MISC
- Any help is appreciated, the main requirement it should run in devices with limited resources like raspberry pi 3