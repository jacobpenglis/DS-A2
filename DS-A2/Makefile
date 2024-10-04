
BIN_DIR = ./bin
DATA_DIR = ./data
LIB_DIR = ./lib
SRC_DIR = ./src
TEST_DIR = ./Test

SOURCES = $(wildcard $(SRC_DIR)/*.java)

JUNIT_JAR = $(LIB_DIR)/junit-4.13.2.jar
HAMCREST_JAR = $(LIB_DIR)/hamcrest-core-1.3.jar
CLASSPATH = $(JUNIT_JAR):$(HAMCREST_JAR)

all: compile

compile:
	javac -d $(BIN_DIR) $(SOURCES)
run_server:
	java -cp $(BIN_DIR) AggregationServer
run_content:
	java -cp $(BIN_DIR) ContentServer http://localhost:4567 ./data/server_error_data.txt
run_get:
	java -cp $(BIN_DIR) GETClient http://localhost:4567 IDS60901

LIBS = $(LIB_DIR)/junit-4.13.2.jar:$(LIB_DIR)/hamcrest-core-1.3.jar

test: $(TEST_DIR) *
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testValidPut01.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testValidPut02.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testValidPut03.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testInvalidPut01.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testInvalidPut02.java

	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testValidGet01.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testValidGet02.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testValidGet03.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testInvalidGet01.java

	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testServerStartup.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testServerStartup02.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testMaxDataEntries.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testConcurrency.java
	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testConcurrency02.java

	javac -d $(BIN_DIR) -cp $(LIBS):$(BIN_DIR) $(TEST_DIR)/testDataExpiry.java
 
	
make run_tests: testValidPut01 testValidPut02 testValidPut03 testInvalidPut01 testInvalidPut02 testValidGet01 testValidGet02 testValidGet03 testInvalidGet01 testServerStartup testServerStartup02 testDataExpiry testMaxDataEntries testConcurrency testConcurrency02

testValidPut01: 
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testValidPut01

testValidPut02: 
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testValidPut02

testValidPut03: 
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testValidPut03

testInvalidPut01: 
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testInvalidPut01

testInvalidPut02: 
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testInvalidPut02

testValidGet01:
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testValidGet01

testValidGet02:
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testValidGet02

testValidGet03:
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testValidGet03

testInvalidGet01:
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testInvalidGet01

testServerStartup:
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testServerStartup

testServerStartup02:
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testServerStartup02

testDataExpiry:
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testDataExpiry

testMaxDataEntries:
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testMaxDataEntries

testConcurrency: 
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testConcurrency

testConcurrency02:
	java -cp $(BIN_DIR):$(LIBS) org.junit.runner.JUnitCore testConcurrency02

clean:
	rm -f $(BIN_DIR)/*.class


