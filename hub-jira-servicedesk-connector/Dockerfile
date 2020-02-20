FROM node:12

WORKDIR /connectors/jira_service_desk

COPY package*.json ./

EXPOSE 8080

ENV PORT 8080

RUN npm install
# If you are building your code for production
# RUN npm install --only=production

COPY . .

CMD [ "npm", "start", "index.js" ]
