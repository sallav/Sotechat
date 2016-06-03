angular.module('chatApp')
    .controller('userInPoolCtrl', ['$http', '$scope', '$location', 'queueService', 'connectToServer', '$timeout',
        function ($http, $scope, $location, queueService, connectToServer, $timeout) {
            var subscribeToQueue;
            $timeout(function () {
                queueService.setUserState('chat');
                subscribeToQueue.unsubscribe();
                $scope.updateState();
            }, 3000);  // test only

            
            var QUEUEADDRESS = '/toClient/queue/';

            var onMessage = function (response) {
                var parsed = JSON.parse(response.body);
                if (parsed.content === 'etene') {
                    subscribeToQueue.unsubscribe();
                    $scope.updateState();
                }
            };

            var onConnection = function () {
                subscribeToQueue = connectToServer.subscribe(QUEUEADDRESS + queueService.getChannelID(), onMessage);
            };

            var init = function () {
                connectToServer.connect(queueService.getChannelID(), onConnection);
            };

            init();


        }]);