/* myHAB site — theme switch, nav, lightbox, copy buttons, docs TOC */
(function () {
	'use strict';

	/* ---- theme switch ----
	   Light is the default; the <head> script has already applied a stored
	   choice, so this only wires the button. */
	var root = document.documentElement;
	var themeBtn = document.querySelector('.theme-toggle');

	function labelTheme() {
		if (!themeBtn) return;
		var dark = root.getAttribute('data-theme') === 'dark';
		var text = dark ? 'Switch to light theme' : 'Switch to dark theme';
		themeBtn.setAttribute('title', text);
		themeBtn.setAttribute('aria-label', text);
	}

	labelTheme();
	if (themeBtn) {
		themeBtn.addEventListener('click', function () {
			var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
			root.setAttribute('data-theme', next);
			try { localStorage.setItem('myhab-theme', next); } catch (e) { /* private mode */ }
			labelTheme();
		});
	}

	/* ---- version slots ----
	   Every [data-version] element ships with the version that was current when
	   the page was published; this replaces it with the newest published GitHub
	   release. On a 404, a rate limit or no network, the baked-in value stands. */
	var versionSlots = document.querySelectorAll('[data-version]');
	if (versionSlots.length && window.fetch) {
		var VERSION_KEY = 'myhab-version';
		var VERSION_TTL = 6 * 60 * 60 * 1000;

		var showVersion = function (v) {
			Array.prototype.forEach.call(versionSlots, function (el) { el.textContent = v; });
		};

		var cached = null;
		try { cached = JSON.parse(localStorage.getItem(VERSION_KEY)); } catch (e) { /* private mode */ }

		if (cached && cached.v && (Date.now() - cached.t) < VERSION_TTL) {
			showVersion(cached.v);
		} else {
			fetch('https://api.github.com/repos/kirpi4ik/myhab/releases/latest', {
				headers: { Accept: 'application/vnd.github+json' }
			})
				.then(function (r) { return r.ok ? r.json() : Promise.reject(r.status); })
				.then(function (data) {
					var v = String(data.tag_name || '').replace(/^v/, '');
					if (!/^\d+\.\d+(\.\d+)?$/.test(v)) return;
					showVersion(v);
					try {
						localStorage.setItem(VERSION_KEY, JSON.stringify({ v: v, t: Date.now() }));
					} catch (e) { /* private mode */ }
				})
				.catch(function () { /* keep the published value */ });
		}
	}

	/* ---- mobile nav ---- */
	var toggle = document.querySelector('.nav-toggle');
	var nav = document.querySelector('.main-nav');
	if (toggle && nav) {
		toggle.addEventListener('click', function () {
			var open = nav.classList.toggle('open');
			toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
		});
	}

	/* ---- lightbox for screenshots ---- */
	var box = document.createElement('div');
	box.className = 'lightbox';
	box.innerHTML = '<div><img alt=""><p></p></div>';
	document.body.appendChild(box);
	var boxImg = box.querySelector('img');
	var boxCap = box.querySelector('p');

	function openBox(src, caption) {
		boxImg.src = src;
		boxImg.alt = caption || '';
		boxCap.textContent = caption || '';
		box.classList.add('open');
		document.body.style.overflow = 'hidden';
	}

	function closeBox() {
		box.classList.remove('open');
		document.body.style.overflow = '';
		boxImg.src = '';
	}

	box.addEventListener('click', closeBox);
	document.addEventListener('keydown', function (e) {
		if (e.key === 'Escape') closeBox();
	});

	Array.prototype.forEach.call(document.querySelectorAll('.shot'), function (fig) {
		fig.addEventListener('click', function () {
			var img = fig.querySelector('img');
			var cap = fig.querySelector('figcaption b');
			if (img) openBox(img.getAttribute('data-full') || img.src, cap ? cap.textContent : img.alt);
		});
	});

	/* ---- copy buttons on code blocks ---- */
	Array.prototype.forEach.call(document.querySelectorAll('.codeblock'), function (block) {
		var pre = block.querySelector('pre');
		if (!pre) return;
		var btn = document.createElement('button');
		btn.className = 'copy';
		btn.type = 'button';
		btn.textContent = 'Copy';
		btn.addEventListener('click', function () {
			var text = pre.innerText;
			var done = function () {
				btn.textContent = 'Copied';
				btn.classList.add('done');
				setTimeout(function () {
					btn.textContent = 'Copy';
					btn.classList.remove('done');
				}, 1600);
			};
			if (navigator.clipboard) {
				navigator.clipboard.writeText(text).then(done);
			} else {
				var ta = document.createElement('textarea');
				ta.value = text;
				document.body.appendChild(ta);
				ta.select();
				document.execCommand('copy');
				document.body.removeChild(ta);
				done();
			}
		});
		block.appendChild(btn);
	});

	/* ---- docs: build "on this page" from h2/h3 + scrollspy ---- */
	var toc = document.querySelector('.docs-toc');
	var main = document.querySelector('.docs-main');
	if (toc && main) {
		var heads = main.querySelectorAll('h2[id], h3[id]');
		if (heads.length) {
			var ul = document.createElement('ul');
			Array.prototype.forEach.call(heads, function (h) {
				var li = document.createElement('li');
				var a = document.createElement('a');
				a.href = '#' + h.id;
				a.textContent = h.textContent.replace('#', '').trim();
				if (h.tagName === 'H3') a.style.paddingLeft = '1.6rem';
				li.appendChild(a);
				ul.appendChild(li);
			});
			toc.appendChild(ul);

			var links = toc.querySelectorAll('a');
			var spy = function () {
				var top = window.scrollY + 120;
				var current = null;
				Array.prototype.forEach.call(heads, function (h) {
					if (h.offsetTop <= top) current = h.id;
				});
				Array.prototype.forEach.call(links, function (a) {
					a.classList.toggle('active', a.getAttribute('href') === '#' + current);
				});
			};
			window.addEventListener('scroll', spy, { passive: true });
			spy();
		}
	}

	/* ---- docs: heading anchor links ---- */
	if (main) {
		Array.prototype.forEach.call(main.querySelectorAll('h2[id], h3[id]'), function (h) {
			var a = document.createElement('a');
			a.className = 'anchor';
			a.href = '#' + h.id;
			a.textContent = '#';
			a.setAttribute('aria-hidden', 'true');
			h.appendChild(a);
		});
	}
})();
