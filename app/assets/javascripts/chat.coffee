$ () ->
	
	urlArgs = ninja
		.tokens
		.get()
		.map (s) -> 'tokens=' + s
		.join("&")
	
	sock = new WebSocket("ws://" + location.host + "/socket?" + urlArgs);
	
	$out = $('#chat-out')
	$in = $('#chat-in')
	
	$out.appendWithScrolling = (html) ->
		isScrolledToBottom = $out[0].scrollHeight - $out[0].clientHeight <= $out[0].scrollTop + 1
		$out.append(html)
		if isScrolledToBottom
			$out[0].scrollTop = $out[0].scrollHeight - $out[0].clientHeight
			
	$('#chat-in').keydown (e) ->
		if e.keyCode == 13
			message = 
				userTokens: ninja.tokens.get()
				content: $(this).val()
				
			sock.send(JSON.stringify(message))
			
			$(this).val('')
			
			e.preventDefault()
			
	window.onbeforeunload = (e) ->
		sock.close()
		
	###
		~~ Socket Event Handlers ~~
	###
	
	sock.onerror = (error) ->
		$in.attr('disabled','true')
		html = 
			'<p class="chat-error">' +
				'Oh noes! A colony of monkeys just ran away with the socket connection.' +
			'</p>'
		$out.appendWithScrolling(html)
		
		
	sock.onmessage = (event) ->
		message = JSON.parse(event.data)
		if message.notification
			html = 
				'<p class="server-msg">' +
					message.notification +
				'</p>'
		else
			html = 
				'<p>' +
					'<u>' + message.name + '</u> ' +
					message.content +
				'</p>'
		
		$out.appendWithScrolling(html)
		
		
	sock.onopen = (e) ->
		console.log('Socket connected!')
	
	
	
	
