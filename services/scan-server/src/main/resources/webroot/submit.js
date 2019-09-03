// Submit scan
$(function()
{
    $('#submit').click(function()
    {
        var name = $('input[name=name]').val();
        var commands = $('textarea').val();
        var queue = $('input[name=queue]').prop('checked');
        var pre_post = $('input[name=pre_post]').prop('checked');
        var url = '/scan/' + encodeURIComponent(name);
        var flags = false;
        if (! queue)
        {
            url += flags ? '&' : '?';
            url += "queue=false";
            flags = true;
        }
        if (! pre_post)
        {
            url += flags ? '&' : '?';
            url += "pre_post=false";
        }   
        $.ajax(
        {
            type: 'POST',
            url: url,
            processData: false,
            contentType: 'text/xml',
            data: commands,
            error: function(xhr, status, message)
            {
                var message = $(xhr.responseXML).find("message").text()
                var trace = $(xhr.responseXML).find("trace").text()
                alert(message + trace);
            },
            success: function(xml)
            {
                var id = +$(xml).find('id').text();
                alert('Submitted: ' + id);
                document.location = '/scans.html';
            }
        });
    });
});
