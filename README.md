pre-reqs
1. java jdk 17 or newer
2. MySQL/ MariaDB installed (im pretty sure either work)
- during setup, take note of your user and password.
3. Gradle (not required)

steps to run
1. clone the repo

2. in the root folder of the repo, run the following command to create the database
mysql -u root -p < src/main/resources/schema.sql
you can also at any time rerun this command to clear all data

3. setup your db.properties file with your user and password
located in src/main/resources/db.properties
i put an example as well but basically just change your user and password to match
what you setup when installing MySQL/MariaDB


4. run the command to start app
gradlew run
