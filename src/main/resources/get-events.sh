#!/bin/bash


if [ "$#" -ne 3 ]; then
    echo "Illegal number of parameters";
    echo "Usage: ./get-events.sh <YYYY> <MM> /path/to/dest/dir"
    echo "Example: ./get-events.sh 2016 03 /path/to/dest/dir"
    exit 1;
fi

#TODO validation of input data
YEAR=$1;
MONTH=$2;
DEST=$3;

if [ ! -d "$DEST" ]; then
  echo "Directory $DEST does not exists! Creating ..."
  mkdir $DEST
  if [ $? -ne 0 ]; then
    echo "Creation of $DEST failed! Exiting"
    exit 1;
  fi
fi


NODAYS=`date -d "$YEAR-$MONTH-01 +1 month -1 day" "+%d"`;
START=`date`
for i in $(seq -f '%02g' 1 $NODAYS)
do
   for j in {0..23}
   do
      FILENAME="$YEAR-$MONTH-$i-$j.json.gz"
      URL="http://data.gharchive.org/$FILENAME"
      #echo $URL
      wget -P $DEST $URL
      if [ $? -ne 0 ]; then
        echo "WARNING: download of $URL failed! Exiting"
        exit 1;
      fi
   done
done
STOP=`date`

echo "started $START";
echo "completed $STOP";
