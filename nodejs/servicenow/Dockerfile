FROM node:alpine

WORKDIR /usr/src/app

COPY package.json .

RUN npm install

COPY . .

EXPOSE 3801

CMD ["npm", "start"]
