# test data and playing around

source("R/decode_barcode.R")

# # test data from phone as a batch
library(foreach)
results <- foreach(col=names(testdata), .combine=rbind) %do% {
  cbind(data.frame(col=col), parse.isbn(rev(testdata[[col]])))
}
pct <- sum(!is.na(results$isbn)) / nrow(results) * 100

nums <- rev(testdata$X1)
plotlines(scales::rescale(nums))
thr <- threshold.amp(nums, plot=T, nwindows=50)
plotbars(thr)
parse.isbn(nums)

parse.isbn(picnums)
plotbars(threshold.amp(picnums, threshold = 0.4, plot=T))