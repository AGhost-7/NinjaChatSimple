$ () ->
	
	sock = new WebSocket("ws://" + location.host + "/socket");
	
	$out = $('#chat-out')
	
	sock.onerror = (error) ->
		console.log('Oh noes! A colony of monkeys just ran away with the socket connection.')
		
	sock.onmessage = (event) ->
		message = JSON.parse(event.data)
		console.log('Server says: ' + event.data)
		html = 
			'<p>' +
				'<u>' + message.name + '</u> ' +
				message.content +
			'</p>'
		$out.append(html)
	
	sock.onopen = (e) ->
		console.log('Socket connected!')
	
	$('#chat-in').keydown (e) ->
		if e.keyCode == 13
		
			message = 
				userTokens: ninja.tokens.get()
				content: $(this).val()
				
			sock.send(JSON.stringify(message))
			
			$(this).val('')
			
			e.preventDefault()
