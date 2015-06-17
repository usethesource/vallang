#!/bin/bash

sed -i '' -E 's/private final Object ((key|val)[[:digit:]]+)/private final int \1/g' $1
sed -i '' -E 's/this.((key|val)[[:digit:]]+) = /this.\1 = \(Integer\) /g' $1

# class Test {
#
# 	private final Object key1;
# 	private final Object key15;
#
# 	private final Object val5;
# 	private final Object val20;
#
# 	Test() {
# 		this.key1 = key1;
# 		this.key15 = key15;
#
# 		this.val5 = val5;
# 		this.val20 = val20;
# 	}
#
# }