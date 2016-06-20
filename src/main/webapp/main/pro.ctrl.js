/** Controlleri huolehtii käyttäjän tilan pyyntämisestä
 *  sivulle tulon yhteydessä, sekä sitä pyytäessä
 */
angular.module('chatProApp')
    .controller('proCtrl', ['$scope', 'auth',
        function ($scope, auth) {
            var CPTEMPLATE = 'proControlPanel/controlPanel.tpl.html';
            var LOGINTEMPLATE = 'login/login.tpl.html';
            $scope.login = function(authenticated) {
                if (authenticated) {
                    $scope.authStatus = CPTEMPLATE;
                    $scope.error = false;
                } else {
                    $scope.error = true;
                }
            };
            
            $scope.logout = function() {
                auth.clear(function() {
                    auth.authenticate([], init);
                });
            };

            var init  = function() {
                if (auth.getAuthStatus() !== false) {
                    $scope.authStatus = CPTEMPLATE;
                } else {
                    $scope.error = false;
                    $scope.authStatus = LOGINTEMPLATE;
                }
            };
            auth.authenticate([], init);
        }]);