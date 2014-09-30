START=$(date +%s.%N)
java -Xmx1500m -cp ".:../../lucene/lucene-4.3.0/*" QryEval Sample.param
END=$(date +%s.%N)
DIFF=$(echo "$END - $START" | bc)
