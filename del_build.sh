#!/bin/bash
function readDir(){
    for element in `ls $1`
    do
        dir_or_file=$1"/"$element
        if [ -d $dir_or_file ]
        then
            if [ $element = "build" ]
            then
                echo "$dir_or_file"
                rm -rfv $dir_or_file
            else
                readDir $dir_or_file
            fi
        fi
    done
}
readDir $1

