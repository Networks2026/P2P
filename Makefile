ARG := $(word 2,$(MAKECMDGOALS))

bash:
	javac -d . $(shell find src -name "*.java")
	java peerProcess $(ARG)

%:
	@:
