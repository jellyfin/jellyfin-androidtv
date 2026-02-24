FROM eclipse-temurin:21-jdk-jammy

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

RUN apt-get update && apt-get install -y \
    unzip \
    wget \
    git \
 && rm -rf /var/lib/apt/lists/*

# Download Android Command Line Tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
 && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O android_tools.zip \
 && unzip -q android_tools.zip -d ${ANDROID_HOME}/cmdline-tools \
 && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
 && rm android_tools.zip

# Accept licenses and install platform-tools (and other packages if needed, though gradle usually fetches them)
# We accept licenses beforehand to avoid build failures
RUN yes | sdkmanager --licenses

WORKDIR /project
