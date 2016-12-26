all: finalRpt

finalRpt: finalRpt.java
	javac -Xlint:all finalRpt.java

run: finalRpt
	java -cp ".;db2jcc4.jar;db2jcc_license_cisuz.jar" finalRpt

doc: finalRpt.java
	javadoc -private finalRpt.java

clean:
	rm -f *.class *.html *.js *.css package-list
