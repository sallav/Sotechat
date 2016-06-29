/** Palvelu huolehtii websocket yhteyden muodostus pyynnöstä
 *  palvelimeen ja pitää yhteyden elossa 
 *  kontrollerien välillä liikkuessa
 */
angular.module('commonMod')
    .service('connectToServer', ['stompSocket', '$timeout', '$window',
        function (stompSocket, $timeout, $window) {
        /** Serverin mappaukset */
        var WEBSOCKETURL = '/toServer';
        /** Yhteyden tila */
        var connectionStatus = false;
        var reconnectMulti = 1;
        /** Yhteyden muodostamis pyyntö 
         *  Parametrina functio jota kutsutaan 
         *  kun yhteys muodostettu
         */
        function connect(answer) {
            if (!connectionStatus) {
                stompSocket.init(WEBSOCKETURL);
                stompSocket.connect(function () {
                    connectionStatus = true;
                    answer();
                }, function () {
                    connectionStatus = false;
                    reconnectMulti *= 2 ;
                    $timeout(function() {
                        connect(function() {
                            $window.location.reload();
                        });
                    }, 10000 * reconnectMulti);
                });
            } else {
                answer();
            }
        }

        /** Funtio jolta voidaan pyytää kanavan tilaamista
         *  Parametrina kavan osoite sekä functio jota kutsutaan
         *  kun viestejä saapuu kanavalta
         */
        function subscribe(destination, answerFunction) {
            return stompSocket.subscribe(destination, function (response) {
                answerFunction(response);
            });
        }

        var socket = {
            connect: connect,
            subscribe: subscribe,
        };

        return socket;
    }]);