FROM node:11

# Create app directory
WORKDIR /connectors/jira_service_desk

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./

# Expose the port on Container
EXPOSE 8080

# Set the PORT env variable
ENV PORT 8080

RUN npm install
# If you are building your code for production
# RUN npm install --only=production

# Bundle app source
COPY . .

CMD [ "npm", "start" ]
