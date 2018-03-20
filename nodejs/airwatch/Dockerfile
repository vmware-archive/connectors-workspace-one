FROM node:alpine

WORKDIR /usr/src/app

COPY package.json .

RUN npm install

COPY . .

# EXPOSE 4001

CMD ["npm", "start", "--", "--vIdmPubKeyUrl=https://herocard.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem", "--greenBoxUrl=https://herocard.vmwareidentity.com"]
