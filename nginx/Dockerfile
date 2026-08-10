FROM nginx:alpine
COPY upstream.conf /etc/nginx/conf.d/upstream.conf
COPY default.conf /etc/nginx/conf.d/default.conf
# 공통 location 블록 — conf.d 밖(snippets)에 두어 자동 로드 대상에서 제외
COPY locations.conf /etc/nginx/snippets/locations.conf
