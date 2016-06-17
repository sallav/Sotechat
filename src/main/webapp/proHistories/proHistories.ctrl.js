/**
 * Created by varkoi on 17.6.2016.
 */

angular.module('chatApp')
        .controller('proHistoriesController', [$scope, 'proStateService','proHistoryService',
            function($scope, proStateService, proHistoryService) {

                /** kuinka paljon keskusteluja naytetaan */
                $scope.quantity = 10;
                /** kuinka paljon viesteja naytetaan */
                $scope.messageQuantity = 25;
                $scope.Conversations = proHistoryService.getHistory();
                if($scope.Conversations.length>0){
                    $scope.left = true;
                }
                $scope.messages = [];

                var addQuantity = function(){
                    if($scope.quantity<$scope.Conversations.length) {
                        $scope.quantity += 10;
                    }else{
                        $scope.left = false;
                    }
                };

                var resetQuantity = function(){
                    $scope.quantity = 10;
                };
                
                var addMessageQuantity = function(){
                    if($scope.messageQuantity<$scope.messages.length) {
                        $scope.messageQuantity += 10;
                    }else {
                        $scope.messagesLeft = false;
                    }
                }
                
                var resetMessageQuantity = function(){
                    $scope.messageQuantity = 25;
                }

                var showConversation = function(channelId) {
                    $scope.messages = proHistoryService.getMessages(channelId);
                    if($scope.messages.length>0){
                        $scope.messagesLeft = true;
                    }
                    return proHistoryService.getConversation(channelId);
                };
                
                var backToConversations = function(){
                    resetQuantity();
                    resetMessageQuantity();
                    return proHistoryService.getHistoryPage();
                };
            }
        ]);