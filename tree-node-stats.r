#!/usr/bin/env Rscript
setwd("~/Development/rascal-devel/pdb.values")

stats <- read.csv("tree-node-stats.csv", sep=",", header=TRUE)

boxplot(stats$size ~ (stats$nodeArity))
boxplot(stats$size ~ (stats$valueArity))

hist(stats$arity, breaks=seq(from=0, to=32))

boxplot(stats$size ~ (stats$arity))





# selection

#selection <- stats[stats$nodeArity == 1,]
#selection <- subset(stats, stats$arity >= 5) # (stats$nodeArity == 1) & (stats$valueArity == 1)
selection <- subset(stats, (stats$nodeArity == 0) & (stats$valueArity >= 5))
hist(selection$arity, breaks=seq(from=0, to=32))
boxplot(selection$size ~ (selection$arity))
