FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN mkdir -p bin && find src -name "*.java" > sources.txt && javac -d bin @sources.txt

EXPOSE 10000

CMD ["java", "-cp", "bin", "ambulance.WebServer"]