$(function() {
    var url = "${createLink(uri: '/stomp')}";
    var csrfHeaderName = "${request._csrf.headerName}";
    var csrfToken = "${request._csrf.token}";
    var socket = new SockJS(url);
    var client = webstomp.over(socket);
    var headers = {};
    headers[csrfHeaderName] = csrfToken;
    client.connect(headers, function() {
        // subscriptions etc. [...]
    });
});