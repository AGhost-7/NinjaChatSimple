do ->
	app = {}
	
	# global namespace for my app...
	window.ninja = app
	
	ajaxDebug = (xhr) ->
		re = JSON.parse(xhr.responseText)
		console.log(re)
	
	# Object auto-persists to localStorage api. Also caps the number
	# of tokens to 5 for the client to keep things speedy.
	app.tokens = do ->
		self = {}
		str = localStorage.getItem("tokens")
		
		if str != "" && str != null
			arr = JSON.parse(str).filter (e) -> !!e
		else
			arr = []
		
		self.add = (t) ->
			arr = ([t].concat(arr)).slice(0,5)
			localStorage.setItem("tokens", JSON.stringify(arr))
		
		self.get = () ->
			arr
			
		self.clear = () ->
			arr = []
			localStorage.setItem("tokens", "[]")
		
		self
	
	app.userName = do ->
		val = undefined
		callbacks = []
		
		get: () ->
			val
		set: (newVal) ->
			val = newVal
			callback(newVal) for callback in callbacks
		listen: (callback) ->
			callbacks.push(callback)
		
	app.userName.listen (newName) ->
		if newName == undefined
			$('#name-span').text('newcomer')
			logout.nav(false)
			login.nav(true)
			register.nav(true)
		else
			$('#name-span').text(newName)
			logout.nav(true)
			login.nav(false)
			register.nav(false)
		
	formState = ''
	
	# First, going to need to check if the user is valid with the tokens
	# that it currently has. Client has to verify on its own what it can 
	# do with its stored state.
	routes.controllers.Users.name(app.tokens.get()).ajax(
		success: (name) ->
			app.userName.set(name)
		error: ajaxDebug
	)
	
	class NavElem
		constructor: (@$a) ->
			
		nav: (bool) ->
			if(bool)
				@$a.parent().removeClass('hidden')
			else
				@$a.parent().addClass('hidden')
				
				
	login = new NavElem($('#login-nav'))
	
	register = new NavElem($('#register-nav'))
	
	logout = new NavElem($('#logout-nav'))
	
	login.$a.click (e) -> 
		navbarAlert.clear()
		formState = 'login'
		
	register.$a.click (e) -> 
		navbarAlert.clear()
		formState = 'register'
	
	logout.$a.click (e) ->
		routes.controllers.Users.logout(app.tokens.get()).ajax(
			success: (response) ->
				app.userName.set(undefined)
		)
		e.preventDefault()
	
	navbarAlert = (message) ->
		html = 
			'<div class="alert alert-danger" role="alert">' +
				'<button type="button" class="close" data-dismiss="alert" aria-label="Close">' +
					'<span aria-hidden="true">&times;</span>' +
				'</button>' +
				message +
			'</div>'
			
		navbarAlert.$cont.html(html)
		
	navbarAlert.$cont = $('#nav-alert-container')
	
	navbarAlert.clear = () ->
		navbarAlert.$cont.html('')
	
	$('#nav-ok-btn').click (e) ->
	
		$inName = $('#nav-in-name')
		$inPw = $('#nav-in-pw')
		name = $inName.val()
		pw = $inPw.val()
		
		if formState == 'register'
			method = routes.controllers.Users.register
		else
			method = routes.controllers.Users.login
		
		method(name, pw).ajax(
			success: (token) ->
				app.userName.set(name)
				app.tokens.add(token._id)
				$('#nav-input').collapse('hide')
				$inPw.val('')
				$inName.val('')
			error: (xhr) ->
				re = JSON.parse(xhr.responseText)
				navbarAlert(re.message)
		)