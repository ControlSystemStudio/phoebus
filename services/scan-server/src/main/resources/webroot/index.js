// 'index' page support
$(function()
{
    $('#shutdown').click(function()
    {
        if (confirm("Shut scan server down?"))
            document.location = '/server/shutdown'
        return false;
    });
});
