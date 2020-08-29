# Copyright 2020 Aleksei Balan
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM maven:3.6-openjdk-8 AS build
COPY . ./
RUN mvn clean package

FROM openjdk:8-alpine
COPY --from=build /target/usfs.jar /usfs.jar
EXPOSE 21
CMD ["java", "-jar", "/usfs.jar", "--folder=/mnt"]