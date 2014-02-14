#!/usr/bin/env Rscript
setwd("~/Development/rascal-devel/pdb.values")

stats <- read.csv("tree-node-stats.csv", sep=",", header=TRUE)

boxplot(stats$size ~ (stats$nodeArity))
boxplot(stats$size ~ (stats$valueArity))

hist(stats$arity, breaks=seq(from=0, to=32))

boxplot(stats$size ~ (stats$arity))