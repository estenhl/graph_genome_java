var Demo = React.createClass({
	renderInputSequences: function () {
		var rows = [];
		for (var i = 0; i < 10; i++) {
			var name = "Sequence_" + i;
			rows.push(<Input name={name} key={name}/>);
		}
		return <div>Input Sequences:<br />{rows}</div>;
	},
	renderAlignmentSequence: function () {
		return <Input name="alignment"/>
	},
	renderErrorMargin: function () {
		return <Select name="select"/>
	},
	onClick: function () {
		var sequences = "";
		for (var i = 0; i < 10; i++) {
			sequences += document.getElementById("Sequence_" + i).value + ",";
		}
		var alignment = document.getElementById("alignment").value;
		var errorMargin = document.getElementById("error-margin").value;
		if (sequences.length < 1) {

		}
		var url = "http://130.211.79.61:8000/test?sequences=" + sequences.substr(0, sequences.length - 2) + "em=errorMargin";
		var xmlHttp = new XMLHttpRequest();
		xmlHttp.onreadystatechange = function () {
			console.log(xmlHttp.responseText);
		};
		xmlHttp.open("GET", url, true); // true for asynchronous
		xmlHttp.setRequestHeader("Access-Control-Allow-Origin", "*");
		xmlHttp.send(null);
		console.log(xmlHttp.responseText);
		return xmlHttp.responseText;
	},
	render: function () {
		var inputSequences = this.renderInputSequences();
		var alignmentSequence = this.renderAlignmentSequence();
		var errorMargin = this.renderErrorMargin();
		return (
			<div>
				{inputSequences}
				<br />
				<br />
				Alignment sequence:<br />
				{alignmentSequence}
				<br />
				<br />
				Error margin:<br />
				{errorMargin}
				<br />
				<br />
				<Button name="Produce graph and align" onClick={this.onClick}/>
			</div>
		)
	}
});

var Input = React.createClass({
	render: function () {
		console.log("Name: " + this.props.name);
		return (
			<div><input type="text" id={this.props.name}/> < br/>
			</div>
		)
	}
});

var Select = React.createClass({
	renderOptions: function () {
		var rows = [];
		for (var i = 0; i < 10; i++) {
			rows.push(<option value={i}>{i}</option>);
		}
		return (<select defaultValue={3} id="error-margin">{rows}</select>);
	},
	render: function () {
		var options = this.renderOptions();
		return (
			<div>
				{options}
			</div>
		)
	}
});

var Button = React.createClass({
	render: function () {
		return (
			<button onClick={this.props.onClick}>{this.props.name}</button>
		);
	}
});

ReactDOM.render(
	<Demo />,
	document.getElementById('example')
);