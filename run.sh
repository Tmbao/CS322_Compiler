java -cp jlex.jar JLex.Main c.jlex
java -cp javacup.jar java_cup.Main c.cup
javac -Xlint:unchecked -cp .:javacupruntime.jar:jlex.jar *.java
java -cp .:jlex.jar:javacupruntime.jar Checker $1 $2

