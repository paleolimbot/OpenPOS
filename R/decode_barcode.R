
library(dplyr)

plotlines <- function(nums) {
  plot(nums, type='l')
}

plotbars <- function(nums) {
  plotlines(nums)
  rect(0:(length(nums)-1), 0, 1:length(nums), 1, col=paste('grey', as.integer(nums*99)+1), border=NA)
}

checksum.upc <- function(upc) {
  upc <- as.character(upc)
  upc <- sapply(1:nchar(upc), function(i) strtoi(substr(upc, i, i)))
  s1 <- sum(upc[c(1, 3, 5, 7, 9, 11)]) * 3 + sum(upc[c(2, 4, 6, 8, 10)])
  10*(floor(s1 / 10.0) + 1) - s1
}


# setup ISBN numbering
digisbn <- read.csv('R/digisbn.csv', colClasses = c("integer", "character", "character"))
dig1isbn <- read.csv('R/dig1isbn.csv', colClasses = c("character", "integer", "character"))

checksum.isbn <- function(isbn) {
  isbn <- as.character(isbn)
  isbn <- sapply(1:nchar(isbn), function(i) strtoi(substr(isbn, i, i)))
  s1 <- sum(isbn[c(1, 3, 5, 7, 9, 11)]) + sum(isbn[c(2, 4, 6, 8, 10, 12)]) * 3
  10*(floor(s1 / 10.0) + 1) - s1
}

threshold.width <- function(thr) {
  narrowbar <- sapply(2:(length(thr)-1), function(i) {
    (thr[i-1] != thr[i]) && (thr[i+1] != thr[i])
  })
  thr[!narrowbar]
}

threshold.amp <- function(nums) {
  # filter by mean/1.5
  thr <- ifelse(nums > mean(nums), 0, 1)
  limits <- c(min(which(thr == 1)), max(which(thr == 1)))
  thr <- thr[limits[1]:limits[2]]
  threshold.width(thr)
}



parse.isbn <- function(nums, threshold.pxwidth=5) {
  # get runs
  bars <- rle(nums)
  bars <- data.frame(lengths=bars$lengths, values=bars$values)
  # for debug
  bars$end <- cumsum(bars$lengths)
  bars$start <- bars$end - bars$lengths + 1
  
  # ensure sufficient length (59)
  if(nrow(bars) < 59) stop("Not enough bars to decode barcode")
  
  if(!all(bars$values[1:3] == c(1,0,1))) stop("No 010 at start of code")
  barsize <- mean(bars$lengths[1:3])
  if(any((bars$lengths[1:3]/barsize) > 1.25)) stop("010 at start of code is invalid")
  
  # categorize bars
  bars$category <- NA
  bars$category[1:3] <- "guard"
  bars$category[4:27] <- "left"
  
  # check for middle guard 01010
  if(!all(bars$values[28:32] == c(0,1,0,1,0))) stop("No 01010 middle guard")
  barsize <- (barsize * 3 + mean(bars$lengths[28:32]) * 4) / 7
  if(any((bars$lengths[28:32]/barsize) > 1.25)) stop("01010 middle guard is invalid")
  
  # categorize second half
  bars$category[28:32] <- "guard"
  bars$category[33:56] <- "right"
  
  # check for end bar
  if(!all(bars$values[57:59] == c(1,0,1))) stop("No 010 at end of code")
  barsize <- (barsize * 7 + mean(bars$lengths[57:59]) * 3) / 10
  if(any((bars$lengths[57:59]/barsize) > 1.25)) stop("010 at end of code is invalid")
  
  # categorize end bars
  bars$category[57:59] <- "guard"
  
  # update final bar size estimate
  barsize <- (bars$end[59] - bars$start[1]) / (3 + 42 + 5 + 42 + 3) # 95
  
  # translate into binary code
  bars$nbars <- round(bars$lengths/barsize)
  bars$binary <- sapply(1:nrow(bars), function(i) {
    paste0(rep(bars$values[i], bars$nbars[i]), collapse="")
  })
  
  # check for 0 length bars
  if(any(bars$nbars == 0)) stop("Model produced zero length bars (invalid)")
  
  binary <- paste0(bars$binary[bars$category %in% c("left", "right")], collapse="")
  # check for valid length of binary code (42 + 42)
  if(nchar(binary) != 84) stop("Binary code of incorrect length")
  
  code <- data.frame(i=2:13)
  code$binary <- sapply(code$i, function(i) {
    substr(binary, (i-1)*7-6, (i-1)*7)
  })
  code <- code %>% group_by(i) %>% do(digisbn[.$binary==digisbn$code,])
  if(!all(2:13 %in% code$i)) stop("Some binary could not be converted to digits")
  
  digit1 <- paste0(code$scheme[1:6], collapse="")
  digit1 <- dig1isbn[dig1isbn$code==digit1,]
  if(nrow(digit1) != 1) stop("Digit 1 could not be decoded")
  digit1$i <- 1
  code <- rbind(as.data.frame(digit1), as.data.frame(code))
  
  # ta da! just have to check the final digit
  isbn <- paste0(code$dig, collapse="")
  if(checksum.isbn(isbn) != as.integer(substr(isbn, 13, 13))) stop("Checksum for ISBN failed")
  return(isbn)
}

# test with picture
testpic <- jpeg::readJPEG('R/2016-09-25 11.43.53.jpg')
testrow <- data.frame(testpic[, 1500, ])
picnums <- rev((testrow$X1 + testrow$X2 + testrow$X3) / 3)
rm(testpic, testrow)

plotlines(picnums)
plotbars(picnums)
picthr <- threshold.amp(picnums)
plotbars(picthr)
parse.isbn(picthr)

# test data from phone
testdata <- data.frame(t(read.csv('R/output.txt', header = F)))

for(col in names(testdata)) {
  nums <- rev(testdata[[col]])
  plotlines(nums)
  plotbars(nums)
  thr <- threshold.amp(nums)
  plotbars(thr)
  tryCatch(print(parse.isbn(thr)), error=function(e) {
    message(e)
  })
}



lines(nums, col='white')
