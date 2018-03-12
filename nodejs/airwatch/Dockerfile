FROM node:alpine

WORKDIR /usr/src/app

COPY package.json .

RUN npm install

COPY . .

EXPOSE 4001

CMD ["npm", "start", "--", "--port=4001", "--vIdmPubKeyUrl=https://herocard.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem", "--greenBoxUrl=http://10.84.130.47:9977"]
