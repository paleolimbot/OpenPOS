
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
# also have df of bar lengths
isbnvec <- t(sapply(digisbn$code, function(code) {
  sapply(1:7, function(i) as.integer(substr(code, i, i)))
}))
lengthsisbn <- data.frame(t(sapply(1:nrow(isbnvec), function(i) {
  rle(isbnvec[i,])$lengths
})))

checksum.isbn <- function(isbn) {
  isbn <- as.character(isbn)
  isbn <- sapply(1:nchar(isbn), function(i) strtoi(substr(isbn, i, i)))
  s1 <- sum(isbn[c(1, 3, 5, 7, 9, 11)]) + sum(isbn[c(2, 4, 6, 8, 10, 12)]) * 3
  10*(floor(s1 / 10.0) + 1) - s1
}

threshold.amp <- function(nums, threshold=0.5, minrange=0.5, nwindows=12, plot=FALSE) {
  # rescale
  nums <- scales::rescale(smoothed <- stats::filter(nums, c(1, 2, 4, 8, 10, 8, 4, 2, 1), circular=T))
  # apply windowed scaling
  window <- round(length(nums) / nwindows)
  minmax <- t(sapply(1:length(nums), function(i) {
    r <- range(nums[max(c(1, i-window/2)):min(c(length(nums), i+window/2))])
    r[1] <- ifelse((r[2]-r[1]) > minrange, r[1], 0)
    r
  }))
  nums <- sapply(1:length(nums), function(i) scales::rescale(nums[i], from=minmax[i,]))
  if(plot) {
    plotlines(nums)
  }
  # re-threshold at 0.5 and trim
  thr <- ifelse(nums > threshold, 0, 1)
  limits <- c(min(which(thr == 1)), max(which(thr == 1)))
  thr <- thr[limits[1]:limits[2]]
  #plotbars(thr)
  return(thr)
}


getpeaks <- function(x, threshold, scaler=1) {
  peaks <- as.vector(ifelse((x*scaler) > threshold, 1, 0))
  peaks[is.na(peaks)] <- 0
  peaks <- rle(peaks)
  peaks$endi <- cumsum(peaks$lengths)
  peaks$starti <- peaks$endi - peaks$lengths + 1
  peaks$meani <- sapply(1:length(peaks$endi), function(i) {
    floor(mean(c(peaks$starti[i], peaks$endi[i])))
  })
  return(peaks$meani[peaks$values==1])
}

derivprocess <- function(nums, threshold=0.45) {
  # smooth
  smoothed <- stats::filter(nums, c(1, 2, 4, 8, 10, 8, 4, 2, 1)/40)
  # compute derivatives
  d1 <- scales::rescale(stats::filter(smoothed, c(0.5, 0, -0.5)), to=c(-1, 1))
  peaks <- getpeaks(d1, 0.5, 1)
  valleys <- getpeaks(d1, 0.5, -1)
  plotlines(nums)
  segments(peaks, 0, peaks, 1, col="red")
  segments(valleys, 0, valleys, 1, col="blue")
  
  npeaks <- c()
  nvalleys <- valleys[1]
  peaki <- 1
  valleyi <- 2
  peakorvalley <- "peak"
  while((peaki <= (length(peaks)-1)) && (valleyi <= (length(valleys)-1))) {
    if(peakorvalley == "peak") {
      peakloc <- peaks[peaki]
      valleyrange <- c(nvalleys[length(nvalleys)], valleys[valleyi])
      if((peakloc > valleyrange[1]) && (peakloc < valleyrange[2])) {
        npeaks <- c(npeaks, peakloc)
        peaki <- peaki + 1
      } else {
        # need to look for a peak in valley range
        npeaks <- c(npeaks, valleyrange[1] + which.max(d1[valleyrange[1]:valleyrange[2]]))
      }
      peakorvalley <- "valley"
    } else {
      valleyloc <- valleys[valleyi]
      peakrange <- c(npeaks[length(npeaks)], peaks[peaki])
      if((valleyloc > peakrange[1]) && (valleyloc < peakrange[2])) {
        nvalleys <- c(nvalleys, valleyloc)
        valleyi <- valleyi + 1
      } else {
        # need to look for a valley in the peak range
        nvalleys <- c(nvalleys, peakrange[1]+which.min(d1[peakrange[1]:peakrange[2]]))
      }
      peakorvalley <- "peak"
    }
    
  }
  # normalize length
  npeaks <- npeaks[1:min(c(length(npeaks), length(nvalleys)))]
  nvalleys <- nvalleys[1:length(npeaks)]
  plotlines(nums)
  segments(npeaks, 0, npeaks, 1, col="red")
  segments(nvalleys, 0, nvalleys, 1, col="blue")
  
  bars <- data.frame(starti=nvalleys, endi=npeaks, values=1)
  bars <- rbind(bars, data.frame(starti=npeaks[1:(length(npeaks)-1)], endi=nvalleys[2:length(nvalleys)], values=0))
  bars$lengths <- bars$endi - bars$starti
  bars <- bars[order(bars$starti),]
  return(bars)
}


digit.isbn <- function(lengths, values, barsizeest) {
  for(barsize in c(barsizeest, seq(0.7, 1.3, 0.2)*barsizeest)) {
    nbars <- pmax(1, round(lengths/barsize))
    if(sum(nbars) != 7) {
      next
    }
    binary <- paste0(sapply(1:4, function(i) {
      paste0(rep(values[i], nbars[i]), collapse="")
    }), collapse="")
    row <- digisbn[binary==digisbn$code,]
    if(nrow(row) == 1) {
      cat(barsize, "/")
      return(row)
    }
  }
  return(data.frame(dig=NA, scheme=NA, code=NA))
}

digit.isbn2 <- function(lengths, values, barsize) {
  # this just based on distances
  nbars <- pmax(1, round(lengths/barsize))
  distest <- sapply(1:nrow(lengthsisbn), function(i) {
    sum((lengthsisbn[i,] - nbars)^2)
  })
  return(digisbn[which.min(distest),])
}

parse.isbn2 <- function(nums) {
  bars <- derivprocess(nums)
  if(nrow(bars) < 59) stop("Not enough bars to process")
  parse.isbn.real(bars)
}

parse.isbn <- function(nums, thresholds=c(0.5, 0.4, 0.6, 0.3, 0.7)) {
  # try a few thresholds
  result <- NULL
  for(threshold in thresholds) {
    thr <- threshold.amp(nums, threshold = threshold)
    # get runs
    bars <- rle(thr)
    bars <- data.frame(lengths=bars$lengths, values=bars$values)
    # for debug
    bars$end <- cumsum(bars$lengths)
    bars$start <- bars$end - bars$lengths + 1
    
    if(bars$values[1] == 0) {
      bars <- bars[2:nrow(bars),]
    }
    
    # ensure sufficient length (59)
    if(nrow(bars) < 59) {
      next
    }
    # try all possible starts
    for(i in seq(0, min(c(5, nrow(bars)-59)), 2)) {
      message("Trying threshold=", threshold, "/i=", i)
      result <- tryCatch(parse.isbn.real(bars), error=function(err) {
        message("Failed with error ", err)
      })
      if(!is.null(result)) {
        message("Succeeded: ISBN=", result, "\n")
        break
      }
    }
      
    if(!is.null(result)) {
      break
    }
  }
  
  if(is.null(result)) {
    return(data.frame(i=NA, threshold=NA, isbn=NA))
  } else {
    return(data.frame(i=i, threshold=threshold, isbn=result))
  }
} 

parse.isbn.real <- function(bars) {
  
  if(!all(bars$values[1:3] == c(1,0,1))) stop("No 010 at start of code")
  barsize <- mean(bars$lengths[1:3])
  if(any((bars$lengths[1:3]/barsize) > 3)) stop("010 at start of code is invalid")
  
  # categorize bars, number digits
  bars$category <- NA
  bars$digit <- NA
  
  bars$category[1:3] <- "guard"
  bars$category[4:27] <- "left"
  bars$digit[4:27] <- expand.grid(n=1:4, d=2:7)$d
  
  # check for middle guard 01010
  if(!all(bars$values[28:32] == c(0,1,0,1,0))) stop("No 01010 middle guard")
  barsize <- (barsize * 3 + mean(bars$lengths[28:32]) * 4) / 7
  if(any((bars$lengths[28:32]/barsize) > 3)) stop("01010 middle guard is invalid")
  
  # categorize second half
  bars$category[28:32] <- "guard"
  bars$category[33:56] <- "right"
  bars$digit[33:56] <- expand.grid(n=1:4, d=8:13)$d
  
  # check for end bar
  if(!all(bars$values[57:59] == c(1,0,1))) warning("No 010 at end of code")
  barsize <- (barsize * 7 + mean(bars$lengths[57:59]) * 3) / 10
  if(any((bars$lengths[57:59]/barsize) > 3)) warning("010 at end of code is invalid")
  
  # categorize end bars
  bars$category[57:59] <- "guard"
  
  # update final bar size estimate
  barsize <- (bars$end[59] - bars$start[1]) / (3 + 42 + 5 + 42 + 3) # 95

  # translate into binary code by digit
  code <- bars %>% filter(!is.na(digit)) %>% group_by(digit) %>%
    do(digit.isbn(.$lengths, .$values, barsize))
  
  # check that all digits were decoded
  if(!all(2:13 %in% code$digit[!is.na(code$dig)])) stop("Not all digits could be decoded: ", paste0(code$dig, collapse="/"))
  
  digit1 <- paste0(code$scheme[1:6], collapse="")
  digit1 <- dig1isbn[dig1isbn$code==digit1,]
  if(nrow(digit1) != 1) stop("Digit 1 could not be decoded")
  digit1$digit <- 1
  code <- rbind(as.data.frame(digit1), as.data.frame(code))
  
  # ta da! just have to check the final digit
  isbn <- paste0(code$dig, collapse="")
  if(checksum.isbn(isbn) != as.integer(substr(isbn, 13, 13))) stop("Checksum for ISBN failed")
  return(isbn)
}

# test with picture
testpic <- jpeg::readJPEG('R/2016-09-25 11.43.53.jpg')
testpic2 <- jpeg::readJPEG('R/codabarpic.jpg')
testrow <- data.frame(testpic[, 1500, ])
testrow2 <- data.frame(testpic2[, 1, ])
picnums <- rev((testrow$X1 + testrow$X2 + testrow$X3) / 3)
picnums2 <- rev((testrow2$X1 + testrow2$X2 + testrow2$X3) / 3)

rm(testpic, testrow, testpic2, testrow2)
# 
# plotlines(picnums)
# plotbars(picnums)
# parse.isbn(picnums)
# 
testdata <- data.frame(t(read.csv('R/output.txt', header = F)))


