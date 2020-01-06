// Simulate scan
$(function()
{
    $('#simulate').click(function()
    {
        $('#simulation').val("Fetching simulation...");
        $('#runtime').text("");

        var commands = $('#scan').val();
        
        $.ajax(
        {
            type: 'POST',
            url: '/simulate/',
            processData: false,
            contentType: 'text/xml',
            data: commands,
            error: function(xhr, status, error)
            {
                var message = $(xhr.responseXML).find("message").text()
                var trace = $(xhr.responseXML).find("trace").text()
                alert(message + trace);
            },
            success: function(xml)
            {
                var log = $(xml).find('log').text();
                var seconds = $(xml).find('seconds').text();
                $('#simulation').val(log);
                $('#runtime').text(seconds);
            }
        });
    });
});
