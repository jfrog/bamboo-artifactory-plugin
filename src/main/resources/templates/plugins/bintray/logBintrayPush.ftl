<html>
<head>
    <meta name="decorator" content="none"/>
</head>
<body>
[#if done]
<div id="pushDone"></div>
[/#if]

<div id="pushLog">
[#if result?has_content]
    [#list result as line]
        <div class="line">${line}</div>
    [/#list]
[#else]
    <div class="line">No logs found.</div>
[/#if]
</div>
</body>
</html>
