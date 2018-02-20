FROM node:alpine

WORKDIR /usr/src/app

COPY package.json .

RUN npm install

COPY . .

EXPOSE 4000

CMD ["npm", "start", "--", "--port=4000", "--vIdmPubKeyUrl=https://herocard.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem"]
