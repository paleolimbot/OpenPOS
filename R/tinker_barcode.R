# test data and playing around

source("R/decode_barcode.R")

# # test data from phone as a batch
library(foreach)
results <- foreach(col=names(testdata), .combine=rbind) %do% {
  cbind(data.frame(col=col), parse.isbn(rev(testdata[[col]])))
}
pct <- sum(!is.na(results$isbn)) / nrow(results) * 100

nums <- rev(testdata$X1)
n2 <- scales::rescale(nums)
plotlines(n2)
thr <- threshold.amp(n2, plot=T)
plotbars(thr)

parse.isbn(picnums)
plotbars(threshold.amp(picnums, plot=T))
