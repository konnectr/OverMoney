var stompClient = null;

$(document).ready(function() {
    connect();

    let timerId = setInterval(sendPrivateMessage, 5000);

});

function connect() {
    const socket = new SockJS('/our-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, frame => {

        console.log('Connected: ' + frame);

        stompClient.subscribe('/user/topic/private-messages', function (message) {
            console.log("тело сообщения: " + message.body)
            if (message.body)
            getUndefinedTransactionsData(); //TODO Делаем запрос на неопределённые транзакции

        });
    });
}

function sendPrivateMessage() {
    console.log("sending private message");
    stompClient.send("/ws-new/private-message", {}, "Приватное сообщение");
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
        console.log("Разсоединение");
    }
}