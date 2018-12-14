Channel Applications
====================

Overview
--------

The Channel Viewer is a CS-Studio application which can query the directory service for a list of channels that match certain conditions, such as physical functionality or location. It also provides mechanisms to create channel name aliases, allowing for different perspectives of the same set of channel names.


Launching
---------

From within cs-studio
``Applications --> Channel --> Channel Table/Channel Tree``

From command line

``-resource cf://?query=SR*&app=channel_tree``
``-resource cf://?query=SR*&app=channel_table``

Channel Table
-------------

Displays the results of a channelfinder query as a table

The query can be based on the channel name or based on a group of tag and properties associated with the channel 

Wildcard character like "*", "?" can be used in the queries  



Channel Tree
------------
Channel Tree by Property allows to create an hierarchical view of the channels by using properties and their values.
It groups the channels returned by a query based on the value of the properties selected.
