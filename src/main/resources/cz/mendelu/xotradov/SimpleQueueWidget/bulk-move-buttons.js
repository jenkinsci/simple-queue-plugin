document.addEventListener('DOMContentLoaded', () => {
    // OnClick code to POST an HTTP request inspired by validateButton() at
    // https://github.com/jenkinsci/jenkins/blob/838cafb8fb86fd843b85d2e22e957e09a1ce86a9/war/src/main/webapp/scripts/hudson-behavior.js#L2581

    document.querySelectorAll('BulkMoveRegex_button_up_fast').forEach(button => {
        button.onclick = function() {
            var parameters = {};
            parameters['${it.getMoveTypeName()}']='UP_FAST';
            parameters['${it.getItemIdName()}']=findPreviousFormItem(this, 'BulkMoveRegex').value;
            fetch('${rootURL}/simpleMove/move', { method: 'post', body: objectToUrlFormEncoded(parameters), headers: crumb.wrap({'Content-Type': 'application/x-www-form-urlencoded',})})
        }
    })

    document.querySelectorAll('BulkMoveRegex_button_down_fast').forEach(button => {
        button.onclick = function() {
            var parameters = {};
            parameters['${it.getMoveTypeName()}']='DOWN_FAST';
            parameters['${it.getItemIdName()}']=findPreviousFormItem(this, 'BulkMoveRegex').value;
            fetch('${rootURL}/simpleMove/move', { method: 'post', body: objectToUrlFormEncoded(parameters), headers: crumb.wrap({'Content-Type': 'application/x-www-form-urlencoded',})})
        }
    })
}
